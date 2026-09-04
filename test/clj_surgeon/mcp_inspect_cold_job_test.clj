(ns ^{:lane :battery} clj-surgeon.mcp-inspect-cold-job-test
  "The inspect tool's COLD-VERIFICATION view -- one test, in the battery lane
   because it launches a child process.

   WHY IT IS ITS OWN NAMESPACE. It used to sit in
   `clj-surgeon.mcp-inspect-tool-test`, which is declared `:fast`, and the
   fast lane's rule is `No child process`. It drives
   `/bin/sh -c 'printf cold-ok'` through `clj-surgeon.mcp-cold-verify` -- the
   production helper, not a stub -- and then polls until the job completes.
   The round-three landing review found it by reading the execution path
   (finding 6); round five's spawn ledger now makes the lane refuse it by pid
   and command line rather than leaving it to a reviewer.

   Two honest options existed: reclassify the whole namespace, or move the one
   test that spawns. Reclassifying would have taken the other thirty-eight
   inspect-tool tests OFF the merge gate to fix one -- the same trade the
   review criticised at a smaller scale. So the spawn moved and everything
   else stayed on the gate. A lane is a statement about what a namespace DOES;
   the fix is to make the statement true, not to make the rule quieter."
  (:require
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-inspect-cold-job-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(deftest callback-queries-a-cold-job-without-rereading-source
  (let [project (temp-dir)
        calls (atom [])]
    (try
      (cold-verify/clear-jobs!)
      (inspect-tool/init! {:project-root (.getPath project)})
      (let [launched (cold-verify/launch!
                       (.getPath project) "full"
                       {:command ["/bin/sh" "-c" "printf cold-ok"]
                        :timeout-ms 1000})
            job (:verification_job launched)]
        (loop [attempt 0]
          (when (and (not (:verification_complete
                            (cold-verify/status (.getPath project) job)))
                     (< attempt 100))
            (Thread/sleep 10)
            (recur (inc attempt))))
        (inspect-tool/handle-inspect
          nil
          {"workspace_root" (.getPath project)
           "verification_job" job
           "view" "verification"}
          (fn [content error? structured]
            (swap! calls conj {:content content :error? error?
                               :structured structured})))
        (is (= false (:error? (first @calls))))
        (is (str/starts-with? (first (:content (first @calls)))
                              "inspect_clojure · cold verification\n"))
        (is (number? (get-in @calls [0 :structured :elapsed_ms])))
        (is (str/includes?
              (first (:content (first @calls)))
              (format "request %.2f ms"
                      (get-in @calls [0 :structured :elapsed_ms]))))
        (is (= :passed (get-in @calls [0 :structured :status])))
        (is (true? (get-in @calls [0 :structured :verification_complete])))
        (is (= 0 (get-in @calls [0 :structured :file_read_count] 0))))
      (finally
        (cold-verify/clear-jobs!)
        (inspect-tool/init! nil)
        (delete-tree! project)))))
