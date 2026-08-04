(ns clj-surgeon.file-ops-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.file-ops :as file-ops]
   [clojure.test :refer [deftest is testing]]))

(deftest atomic-write-preserves-existing-file-permissions
  (let [dir (fs/create-temp-dir {:prefix "clj surgeon file mode "})
        file (fs/path dir "executable.clj")]
    (try
      (spit (str file) "(println :before)\n")
      (let [target (.toFile file)]
        (.setExecutable target true false)
        (.setReadable target true false)
        (.setWritable target true true)
        (let [before {:executable (.canExecute target)
                      :readable (.canRead target)
                      :writable (.canWrite target)}]
          (file-ops/atomic-write! (str file) "(println :after)\n")
          (testing "content changes without weakening or discarding its mode"
            (is (= "(println :after)\n" (slurp (str file))))
            (is (= before
                   {:executable (.canExecute target)
                    :readable (.canRead target)
                    :writable (.canWrite target)})))))
      (finally
        (fs/delete-tree dir)))))
