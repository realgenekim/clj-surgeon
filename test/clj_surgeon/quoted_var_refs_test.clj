(ns clj-surgeon.quoted-var-refs-test
  (:require
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(defn- delete-tree!
  [file]
  (doseq [entry (reverse (file-seq file))]
    (.delete entry)))

(deftest recognizes-var-references-by-namespace-identity
  (testing "fully qualified and aliased reader forms share one identity"
    (let [source (str "(ns consumer.core\n"
                      "  (:require [sample.core :as sample]))\n"
                      "(defn aliased [] #'sample/target)\n"
                      "(defn expanded [] (var sample.core/target))\n"
                      "(defn unrelated [] #'other.core/target)\n"
                      "(def text \"#'sample.core/target\")\n"
                      ";; #'sample.core/target\n"
                      "(def data '(var sample.core/target))\n"
                      "(def discarded #_(var sample.core/target) nil)\n"
                      "(def syntax-quoted `(var sample.core/target))\n")
          references (quoted-var-refs/references-in-source
                       "src/consumer/core.clj" source "sample.core/target")]
      (is (= ["#'sample/target" "(var sample.core/target)"]
             (mapv :source references)))
      (is (= #{:structural-var-quote}
             (set (map :reference-authority references))))))

  (testing "an unqualified Var resolves only inside its defining namespace"
    (is (= ["#'target"]
           (mapv :source
                 (quoted-var-refs/references-in-source
                   "src/sample/core.clj"
                   "(ns sample.core)\n(defn caller [] #'target)\n"
                   "sample.core/target"))))
    (is (empty?
          (quoted-var-refs/references-in-source
            "src/consumer/core.clj"
            "(ns consumer.core)\n(defn caller [] #'target)\n"
            "sample.core/target")))))

(deftest captured-source-scan-parses-each-candidate-once-for-many-subjects
  (let [source (str "(ns consumer.core\n"
                    "  (:require [sample.core :as sample]))\n"
                    "(def refs [#'sample/first\n"
                    "           #'sample/second\n"
                    "           (var sample.core/first)])\n")
        subjects ["sample.core/second" "sample.core/first"]
        batch-calls (atom 0)
        batch quoted-var-refs/references-in-source-for-subjects
        result
        (with-redefs
          [quoted-var-refs/references-in-source-for-subjects
           (fn [file captured-source captured-subjects]
             (swap! batch-calls inc)
             (batch file captured-source captured-subjects))]
          (quoted-var-refs/scan-sources
            {"src/consumer/core.clj" source}
            subjects))]
    (is (:ok result))
    (is (= 1 @batch-calls)
        "subject count must not multiply candidate-file parsing")
    (is (= ["sample.core/second"
            "sample.core/first"
            "sample.core/first"]
           (mapv :subject (:locations result))))
    (is (= ["#'sample/second"
            "#'sample/first"
            "(var sample.core/first)"]
           (mapv :source (:locations result))))))

(deftest captured-source-scan-is-pure-bounded-and-deterministic
  (let [sources
        {"src/z.clj"
         (str "(ns z (:require [sample.core :as sample]))\n"
              "(def found #'sample/target)\n")
         "src/a.clj"
         (str "(ns a)\n"
              "(def text \"#'sample.core/target\")\n")
         "src/sample/core.clj"
         (str "(ns sample.core)\n"
              "(def local #'target)\n")}
        result (quoted-var-refs/scan-sources
                 sources ["sample.core/target"])]
    (is (:ok result))
    (is (= 3 (:scanned-file-count result)))
    (is (= 3 (:candidate-file-count result)))
    (is (= ["src/sample/core.clj" "src/z.clj"]
           (mapv :file (:locations result))))
    (is (= 2 (:reference-count result)))
    (is (= (quoted-var-refs/scan-sources
             (into (array-map) (reverse sources))
             ["sample.core/target"])
           result)
        "map iteration order cannot change proof output"))

  (let [result (quoted-var-refs/scan-sources
                 {"src/a.clj" "(ns a) (def x #'sample.core/target)"
                  "src/b.clj" "(ns b) (def x #'sample.core/target)"}
                 ["sample.core/target"]
                 {:max-candidate-files 1})]
    (is (false? (:ok result)))
    (is (= :quoted-var-scan-budget-exceeded (:error-type result)))
    (is (true? (:source-unchanged result))))

  (let [result (quoted-var-refs/scan-sources
                 {"src/broken.clj" "(ns broken) (def x #'sample.core/target"}
                 ["sample.core/target"])]
    (is (false? (:ok result)))
    (is (= :quoted-var-scan-failed (:error-type result)))
    (is (true? (:source-unchanged result)))))

(deftest workspace-proof-refuses-an-invalid-candidate-before-publishing-evidence
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "quoted-var-proof-"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src/broken.clj")]
    (try
      (.mkdirs (.getParentFile source))
      (spit source "(ns broken)\n(def x #'sample.core/target\n")
      (let [result (quoted-var-refs/scan-workspace
                     root ["sample.core/target"] slurp)]
        (is (false? (:ok result)))
        (is (= :quoted-var-scan-failed (:error-type result)))
        (is (:source-unchanged result)))
      (finally
        (delete-tree! root)))))
